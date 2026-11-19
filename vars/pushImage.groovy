def call(String imageName){
    def dockerConfig = "${pwd(tmp: true)}/docker-config-${env.BUILD_TAG ?: System.currentTimeMillis()}"
    withEnv(["DOCKER_CONFIG=${dockerConfig}", "IMAGE_NAME=${imageName}"]) {
        try {
            withCredentials([usernamePassword(credentialsId: "dockerhub", passwordVariable: 'DOCKER_PASSWD', usernameVariable: 'DOCKER_USER')]) {
                sh '''
                    set +x
                    mkdir -p "$DOCKER_CONFIG"
                    chmod 700 "$DOCKER_CONFIG"
                    docker tag "$IMAGE_NAME" "$DOCKER_USER/springboot-application"
                    printf '%s' "$DOCKER_PASSWD" | docker login -u "$DOCKER_USER" --password-stdin
                    docker push "$DOCKER_USER/$IMAGE_NAME"
                '''
            }
        } finally {
            sh '''
                docker logout >/dev/null 2>&1 || true
                rm -rf "$DOCKER_CONFIG"
            '''
        }
    }
}
