{{/*
Renders an immutable image reference "<repository>:<tag>@<digest>".
Fails the render when the tag is missing or "latest", or the digest is not a sha256 digest,
so a mutable tag can never reach the cluster.
Usage: {{ include "bankapp.image" (dict "name" "app" "image" .Values.image.app) }}
*/}}
{{- define "bankapp.image" -}}
{{- $key := printf "image.%s" .name -}}
{{- $repo := required (printf "%s.repository is required" $key) .image.repository | toString -}}
{{- if or (contains "@" $repo) (regexMatch ":[^/]*$" $repo) -}}
{{- fail (printf "%s.repository must not contain a tag or digest; set %s.tag and %s.digest" $key $key $key) -}}
{{- end -}}
{{- $tag := required (printf "%s.tag is required (an immutable version, e.g. the CI DOCKER_TAG)" $key) .image.tag | toString -}}
{{- if eq (lower $tag) "latest" -}}
{{- fail (printf "%s.tag must not be \"latest\"; use a fixed version" $key) -}}
{{- end -}}
{{- $digest := required (printf "%s.digest is required (sha256:<64 hex>, from `docker push` or `docker buildx imagetools inspect`)" $key) .image.digest | toString -}}
{{- if not (regexMatch "^sha256:[a-f0-9]{64}$" $digest) -}}
{{- fail (printf "%s.digest must look like sha256:<64 lowercase hex chars>, got %q" $key $digest) -}}
{{- end -}}
{{- printf "%s:%s@%s" $repo $tag $digest -}}
{{- end -}}
