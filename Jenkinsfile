pipeline {
  agent any

  options {
    timestamps()
    ansiColor('xterm')
    timeout(time: 30, unit: 'MINUTES')
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    skipDefaultCheckout(true)
  }

  environment {
    SONARQUBE_SERVER = 'sonarqube'
    SONAR_TOKEN = credentials('sonar-jenkins-token')

    DB_CONTAINER = 'ci-postgres'
    DB_USER = 'order'
    DB_PASS = 'order'
    DB_NAME = 'order_db'
    PG_PORT_FILE = '.pg_host_port'
    POSTGRES_IMAGE = 'postgres:17.7'
  }

  stages {

    stage('Print OS & Runtime Info') {
      steps {
        sh '''
          set -eux
          uname -a || true
          cat /etc/os-release || true
          java -version || true
          git --version || true
          docker --version || true
        '''
      }
    }

    stage('Checkout') {
      steps {
        checkout scm
        sh '''
          set -eux
          chmod +x gradlew
          sed -i 's/\r$//' gradlew || true
          ./gradlew -v
        '''
      }
    }

    stage('Start PostgreSQL (Test)') {
      steps {
        sh '''
          set -eux

          # Clean any previous leftover
          docker rm -f "$DB_CONTAINER" || true
          rm -f "$PG_PORT_FILE" || true

          # Run Postgres with RANDOM host port to avoid collisions
          docker run -d --name "$DB_CONTAINER" \
            -e POSTGRES_USER="$DB_USER" \
            -e POSTGRES_PASSWORD="$DB_PASS" \
            -p 0:5432 \
            "$POSTGRES_IMAGE"

          HOST_PORT="$(docker port "$DB_CONTAINER" 5432/tcp | awk -F: '{print $2}')"
          echo "Postgres published on host port: ${HOST_PORT}"
          echo "${HOST_PORT}" > "$PG_PORT_FILE"

          # Wait for Postgres to accept connections
          for i in $(seq 1 60); do
            if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" >/dev/null 2>&1; then
              echo "Postgres is ready"
              break
            fi
            sleep 1
          done

          # Fail if still not ready
          docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER"

          # Ensure DB exists (idempotent)
          if ! docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -tc \
            "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'" | grep -q 1; then
            docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE ${DB_NAME};"
          fi

          # Verify
          docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;"
        '''
      }
    }

    stage('Build + Test + JaCoCo') {
      steps {
        sh '''
          set -eux
          DOCKER_HOST_IP="$(ip route | awk '/default/ {print $3}')"
          HOST_PORT="$(cat "$PG_PORT_FILE")"

          echo "DOCKER_HOST_IP=$DOCKER_HOST_IP"
          echo "HOST_PORT=$HOST_PORT"

          # Quick network check (Jenkins-in-Docker needs gateway IP)
          timeout 2 bash -lc "cat < /dev/null > /dev/tcp/${DOCKER_HOST_IP}/${HOST_PORT}"

          export SPRING_APPLICATION_JSON='{
            "spring": {
              "datasource": {
                "url": "jdbc:postgresql://'${DOCKER_HOST_IP}':'${HOST_PORT}'/'${DB_NAME}'",
                "username": "'${DB_USER}'",
                "password": "'${DB_PASS}'",
                "driver-class-name": "org.postgresql.Driver"
              }
            }
          }'

          ./gradlew clean test jacocoTestReport jacocoRootReport
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
          archiveArtifacts allowEmptyArchive: true, artifacts: '''
            **/build/reports/jacoco/**,
            **/build/reports/tests/**,
            **/build/libs/*.jar
          '''
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv("${SONARQUBE_SERVER}") {
          sh '''
            set -eux
            export SONAR_TOKEN="${SONAR_TOKEN}"

            # Tests + root report already done above
            ./gradlew sonar -x test -x jacocoRootReport
          '''
        }
      }
    }
  }

  post {
    always {
      sh '''
        set +e
        docker rm -f "$DB_CONTAINER" >/dev/null 2>&1 || true
        rm -f "$PG_PORT_FILE" || true
      '''
      cleanWs()
    }
  }
}
