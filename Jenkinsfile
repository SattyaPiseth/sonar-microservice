pipeline {
    agent any

    environment {
        SONARQUBE_SERVER = 'sonarqube'
        SONAR_TOKEN = credentials('sonar-jenkins-token')

        // DB config used by Spring tests
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/orders'
        SPRING_DATASOURCE_USERNAME = 'postgres'
        SPRING_DATASOURCE_PASSWORD = 'postgres'
    }

    stages {

        stage('Print OS & Kernel Information') {
            steps {
                sh '''
                    set -eux
                    echo "==== OS Info ===="
                    uname -a || true
                    cat /etc/os-release || true
                    echo "==== Java ===="
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

        stage('Start PostgreSQL (Test)') {
            steps {
                sh '''
                    set -eux

                    # Clean any old container from previous runs
                    docker rm -f ci-postgres || true

                    docker run -d --name ci-postgres \
                      -e POSTGRES_DB=orders \
                      -e POSTGRES_USER=postgres \
                      -e POSTGRES_PASSWORD=postgres \
                      -p 5432:5432 \
                      postgres:17.7

                    # Wait until Postgres is ready
                    for i in $(seq 1 30); do
                      docker exec ci-postgres pg_isready -U postgres -d orders && exit 0
                      sleep 2
                    done

                    echo "Postgres not ready" >&2
                    docker logs ci-postgres || true
                    exit 1
                '''
            }
        }

        stage('Build + Test + JaCoCo') {
            steps {
                sh '''
                    set -eux
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

//        stage('SonarQube Analysis') {
//            steps {
//                withSonarQubeEnv("${SONARQUBE_SERVER}") {
//                    sh '''
//                        set -eux
//                        export SONAR_TOKEN="${SONAR_TOKEN}"
//
//                        # Optional: avoid re-running tests if your sonar task dependsOn tests
//                        # ./gradlew sonar -x test -x jacocoRootReport
//
//                        ./gradlew sonar
//                    '''
//                }
//            }
//        }
    }

    post {
        always {
            // Always cleanup postgres container to avoid port conflicts
            sh 'docker rm -f ci-postgres || true'

            cleanWs()
        }
    }
}
