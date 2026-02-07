pipeline {
    agent any

    environment {
        SONARQUBE_SERVER = 'sonarqube'
        SONAR_TOKEN = credentials('sonar-jenkins-token')
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
                    # Fix permission denied for gradlew
                    chmod +x gradlew
                    # Ensure wrapper script uses unix line endings if it was edited on Windows
                    sed -i 's/\r$//' gradlew || true
                    ./gradlew -v
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
//                        ./gradlew sonar
//                    '''
//                }
//            }
//        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
