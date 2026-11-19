def call(String Project, String ImageTag, String dockerhubuser){
  // Per-build Docker client config so the registry login is never written to the agent user's ~/.docker.
  def dockerConfig = "${pwd(tmp: true)}/docker-config-${env.BUILD_TAG ?: System.currentTimeMillis()}"
  withEnv(["DOCKER_CONFIG=${dockerConfig}", "DOCKER_IMAGE_REF=${dockerhubuser}/${Project}:${ImageTag}"]) {
    try {
      withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'dockerhubpass', usernameVariable: 'dockerhubcreduser')]) {
        sh '''
          set +x
          mkdir -p "$DOCKER_CONFIG"
          chmod 700 "$DOCKER_CONFIG"
          printf '%s' "$dockerhubpass" | docker login -u "$dockerhubcreduser" --password-stdin
          docker push "$DOCKER_IMAGE_REF"
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
