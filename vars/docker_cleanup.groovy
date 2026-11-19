def call(String Project, String ImageTag, String DockerHubUser){
  if (!(DockerHubUser ==~ /^[a-z0-9][a-z0-9_.-]{0,254}$/)) {
    error('Invalid DockerHub user: only lowercase letters, digits, ".", "_" and "-" are allowed')
  }
  if (!(Project ==~ /^[a-z0-9][a-z0-9_.-]{0,254}$/)) {
    error('Invalid project name: only lowercase letters, digits, ".", "_" and "-" are allowed')
  }
  if (!(ImageTag ==~ /^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$/)) {
    error('Invalid image tag: must match ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$')
  }
  withEnv(["DOCKER_IMAGE_REF=${DockerHubUser}/${Project}:${ImageTag}"]) {
    sh 'docker rmi "$DOCKER_IMAGE_REF"'
  }
}
