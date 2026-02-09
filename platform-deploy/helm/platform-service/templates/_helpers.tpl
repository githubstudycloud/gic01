{{- define "platform-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "platform-service.fullname" -}}
{{- /*
Resource naming rule (strict + ergonomic):
- By default, use the Helm release name as the k8s object name.
- This makes `helm upgrade --install <release>` map directly to `deployment/<release>`, `svc/<release>`, etc.
- It also avoids collisions when installing the chart multiple times in one namespace.
*/ -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
