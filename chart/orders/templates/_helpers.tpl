{{- define "messageHubEnv" -}}
  {{- if (index .Values "bluemix-messagehub").enabled -}}
        - name: messagehub
          valueFrom:
            secretKeyRef:
              name: {{ cat "binding-" ((index .Values "bluemix-messagehub").service.name | lower | replace " " "-") | nospace }}
              key: binding
  {{- else if .Values.tags.bluemix -}}
        - name: messagehub
          valueFrom:
            secretKeyRef:
              name: {{ cat "binding-" ((index .Values "bluemix-messagehub").service.name | lower | replace " " "-") | nospace }}
              key: binding
  {{- end -}}
{{- end -}}

{{- define "mysqlBindingName" -}}
  {{- if (index .Values "bluemix-compose-mysql").enabled -}}
    {{- cat "binding-" ((index .Values "bluemix-compose-mysql").service.name | lower | replace " " "-") | nospace -}}
  {{- else if .Values.tags.bluemix -}}
    {{- cat "binding-" ((index .Values "bluemix-compose-mysql").service.name | lower | replace " " "-") | nospace -}}
  {{- else -}}
    {{- (index .Values "ibmcase-mysql").binding.name -}}
  {{- end -}}
{{- end -}}

{{- define "hs256SecretName" -}}
  {{- if .Values.global.hs256key.secretName -}}
    {{- .Release.Name }}-{{ .Values.global.hs256key.secretName -}}
  {{- else -}}
    {{- .Release.Name }}-{{ .Chart.Name }}-{{ .Values.hs256key.secretName -}}
  {{- end }}
{{- end -}}
