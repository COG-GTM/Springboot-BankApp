def call(String Project, String ImageTag, String dockerhubuser){
  withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'dockerhubpass', usernameVariable: 'DOCKERHUB_CRED_USER')]) {
      withEnv(["DOCKERHUB_LOGIN_USER=${dockerhubuser}"]) {
          sh '''
              set +x
              printf '%s' "$dockerhubpass" | docker login -u "$DOCKERHUB_LOGIN_USER" --password-stdin
          '''
      }
  }
  sh "docker push ${dockerhubuser}/${Project}:${ImageTag}"
}
