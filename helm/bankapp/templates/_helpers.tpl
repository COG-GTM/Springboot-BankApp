{{/* Name of the Secret that holds the database passwords. */}}
{{- define "bankapp.dbSecretName" -}}
{{- default .Values.secret.name .Values.secret.existingSecret -}}
{{- end -}}

{{/* Database user the application connects as; never root. */}}
{{- define "bankapp.dbUser" -}}
{{- $user := required "configmap.data.SPRING_DATASOURCE_USERNAME is required" .Values.configmap.data.SPRING_DATASOURCE_USERNAME -}}
{{- if eq $user "root" -}}
{{- fail "configmap.data.SPRING_DATASOURCE_USERNAME must not be root: the application needs its own least-privilege MySQL user" -}}
{{- end -}}
{{- $user -}}
{{- end -}}
