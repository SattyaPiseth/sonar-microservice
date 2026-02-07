pipeline {
    agent any

    environment {
        SONARQUBE_SERVER = 'sonarqube'
        SONAR_TOKEN = credentials('sonar-jenkins-token')

        SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5992/order_db'
        SPRING_DATASOURCE_USERNAME = 'order'
        SPRING_DATASOURCE_PASSWORD = 'order'
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

                    docker run -d --name ci-postgres \
                      -e POSTGRES_DB=order_db \
                      -e POSTGRES_USER=order \
                      -e POSTGRES_PASSWORD=order \
                      -p 5992:5432 \
                      postgres:17.7

                    ready=0
                    for i in $(seq 1 30); do
                      if docker exec ci-postgres pg_isready -U order -d order_db; then
                        ready=1
                        break
                      fi
                      sleep 2
                    done

                    if [ "$ready" -ne 1 ]; then
                      docker logs ci-postgres || true
                      exit 1
                    fi

                    docker exec ci-postgres psql -U order -d order_db -c "SELECT 1;"
                '''
            }
        }

        stage('Build + Test + JaCoCo') {
          steps {
            sh '''
              set -eux

              DOCKER_HOST_IP="$(ip route | awk '/default/ {print $3}')"
              echo "DOCKER_HOST_IP=$DOCKER_HOST_IP"

              ./gradlew clean test jacocoTestReport jacocoRootReport \
                -Dspring.datasource.url="jdbc:postgresql://${DOCKER_HOST_IP}:5992/order_db" \
                -Dspring.datasource.username="order" \
                -Dspring.datasource.password="order"
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

    }

    post {
        always {
            sh 'docker rm -f ci-postgres || true'
            cleanWs()
        }
    }
}
