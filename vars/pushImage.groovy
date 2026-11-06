def call(String imageName){
    withCredentials([usernamePassword(credentialsId: "dockerhub", passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
        withEnv(["IMAGE_NAME=${imageName}"]) {
            sh 'docker tag "$IMAGE_NAME" "$DOCKER_USER/springboot-application"'
            sh '''
                set +x
                printf '%s' "$DOCKER_PASSWD" | docker login -u "$DOCKER_USER" --password-stdin
            '''
            sh 'docker push "$DOCKER_USER/$IMAGE_NAME"'
        }
    }
}
