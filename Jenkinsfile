pipeline {
    agent any

    tools {
        // Optional: use Jenkins tool installers if you configured them
        // jdk 'jdk-21'
        // gradle 'gradle-9'
    }

    environment {
        // Name must match the SonarQube server name you configured in Jenkins
        SONARQUBE_SERVER = 'sonarqube'

        // Recommended: store SONAR_TOKEN as Jenkins "Secret Text" credential
        // Credentials ID example: sonar-token
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
                    echo "==== CPU / Memory ===="
                    nproc || true
                    free -h || true
                    echo "==== Disk ===="
                    df -h || true
                    echo "==== Java ===="
                    java -version || true
                    echo "==== Gradle ===="
                    ./gradlew -v || gradle -v || true
                '''
            }
        }

    }

}
