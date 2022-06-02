pipeline {

    agent any

    triggers {
        cron('H */8 * * *') //regular builds
        pollSCM('* * * * *') //polling for changes, here once a minute
    }

    stages {
        stage('Build') {
            steps {
                script {
                    sh './gradlew clean build' //run a gradle task
                }
            }
        }
//         stage('Publish Artifact to Nexus') {
//             steps {
//                 sh './gradlew publish --no-daemon'
//             }
//         }
    }
    post {
        always { //Send an email to the person that broke the build
            step([
                    $class                  : 'Mailer',
                    notifyEveryUnstableBuild: true,
                    recipients              : [emailextrecipients([[$class: 'CulpritsRecipientProvider'],[$class: 'RequesterRecipientProvider']])].join(' ')
            ])
        }
    }
}
