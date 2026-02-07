pipeline {
    agent any

    environment {
        SONARQUBE_SERVER = 'sonarqube'
        SONAR_TOKEN = credentials('sonar-jenkins-token')

        SPRING_DATASOURCE_USERNAME = 'order'
        SPRING_DATASOURCE_PASSWORD = 'order'
        TEST_DB_NAME = 'order_db'
    }

    stages {
        stage('Print OS & Kernel Information') {
            steps {
                sh '''
                    set -eux
                    uname -a || true
                    cat /etc/os-release || true
                    java -version || true
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

        stage('Detect Docker Host Gateway') {
            steps {
                sh '''
                    set -eux
                    echo "Docker host gateway (default route):"
                    ip route | awk '/default/ {print $3}'
                '''
            }
        }

        stage('Start PostgreSQL (Test)') {
            steps {
                sh '''
                    set -eux
                    docker rm -f ci-postgres || true

                    # Use RANDOM host port to avoid "port already in use" / container restart issues
                    docker run -d --name ci-postgres \
                      -e POSTGRES_USER="$SPRING_DATASOURCE_USERNAME" \
                      -e POSTGRES_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
                      -p 0:5432 \
                      postgres:17

                    HOST_PORT="$(docker port ci-postgres 5432/tcp | awk -F: '{print $2}')"
                    echo "Postgres published on host port: ${HOST_PORT}"
                    echo "${HOST_PORT}" > .pg_host_port

                    # Wait for server ready
                    for i in $(seq 1 60); do
                      if docker exec ci-postgres pg_isready -U "$SPRING_DATASOURCE_USERNAME"; then
                        break
                      fi
                      sleep 1
                    done

                    # Ensure DB exists (retry a few times because init can still be finishing)
                    for i in $(seq 1 10); do
                      if docker exec ci-postgres psql -U "$SPRING_DATASOURCE_USERNAME" -d postgres -tc \
                        "SELECT 1 FROM pg_database WHERE datname='${TEST_DB_NAME}'" | grep -q 1; then
                        echo "Database ${TEST_DB_NAME} exists"
                        break
                      fi

                      echo "Creating database ${TEST_DB_NAME} (attempt $i)..."
                      docker exec ci-postgres psql -U "$SPRING_DATASOURCE_USERNAME" -d postgres -c \
                        "CREATE DATABASE ${TEST_DB_NAME};" && break || true
                      sleep 1
                    done

                    # Verify DB connectivity
                    docker exec ci-postgres psql -U "$SPRING_DATASOURCE_USERNAME" -d "${TEST_DB_NAME}" -c "SELECT 1;"
                '''
            }
        }

        stage('Build + Test + JaCoCo') {
            steps {
                sh '''
                    set -eux
                    DOCKER_HOST_IP="$(ip route | awk '/default/ {print $3}')"
                    HOST_PORT="$(cat .pg_host_port)"

                    echo "DOCKER_HOST_IP=$DOCKER_HOST_IP"
                    echo "HOST_PORT=$HOST_PORT"

                    ./gradlew clean test jacocoTestReport jacocoRootReport \
                      -Dspring.datasource.url="jdbc:postgresql://${DOCKER_HOST_IP}:${HOST_PORT}/${TEST_DB_NAME}" \
                      -Dspring.datasource.username="$SPRING_DATASOURCE_USERNAME" \
                      -Dspring.datasource.password="$SPRING_DATASOURCE_PASSWORD"
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

//        stage('SonarQube Analysis') {
//            steps {
//                withSonarQubeEnv("${SONARQUBE_SERVER}") {
//                    sh '''
//                        set -eux
//                        export SONAR_TOKEN="${SONAR_TOKEN}"
//                        ./gradlew sonar -x test -x jacocoRootReport
//                    '''
//                }
//            }
//        }
    }

    post {
        always {
            sh '''
              docker rm -f ci-postgres || true
              rm -f .pg_host_port || true
            '''
            cleanWs()
        }
    }
}
