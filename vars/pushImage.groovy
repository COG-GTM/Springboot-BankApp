def call(String imageName){
    if (imageName == null || !imageName.matches('^[a-zA-Z0-9][a-zA-Z0-9_.-]*$')) {
       error('Invalid image name. Must contain only alphanumeric characters, dots, dashes, and underscores')
    }

    withEnv(["IMAGE_NAME=${imageName}"]) {
        withCredentials([usernamePassword(credentialsId: "dockerhub", passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
            sh 'docker tag "$IMAGE_NAME" "$DOCKER_USER/springboot-application"'
            sh 'echo "$DOCKER_PASSWD" | docker login -u "$DOCKER_USER" --password-stdin'
            sh 'docker push "$DOCKER_USER/$IMAGE_NAME"'
        }
    }
}
