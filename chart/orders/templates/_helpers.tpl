{{- define "messageHubEnv" -}}
  {{- if (index .Values "bluemixmessagehub").enabled -}}
        - name: messagehub
          valueFrom:
            secretKeyRef:
              name: {{ cat "binding-" ((index .Values "bluemixmessagehub").service.name | lower | replace " " "-") | nospace }}
              key: binding
  {{- else if .Values.tags.bluemix -}}
        - name: messagehub
          valueFrom:
            secretKeyRef:
              name: {{ cat "binding-" ((index .Values "bluemixmessagehub").service.name | lower | replace " " "-") | nospace }}
              key: binding
  {{- end -}}
{{- end -}}

{{- define "mysqlBindingName" -}}
  {{- if (index .Values "bluemixcomposemysql").enabled -}}
    {{- cat "binding-" ((index .Values "bluemixcomposemysql").service.name | lower | replace " " "-") | nospace -}}
  {{- else if .Values.tags.bluemix -}}
    {{- cat "binding-" ((index .Values "bluemixcomposemysql").service.name | lower | replace " " "-") | nospace -}}
  {{- else -}}
    {{- (index .Values "mysql").binding.name -}}
  {{- end -}}
{{- end -}}

{{- define "hs256SecretName" -}}
  {{- if .Values.global.hs256key.secretName -}}
    {{- .Release.Name }}-{{ .Values.global.hs256key.secretName -}}
  {{- else -}}
    {{- .Release.Name }}-{{ .Chart.Name }}-{{ .Values.hs256key.secretName -}}
  {{- end }}
{{- end -}}

{{- define "ordersDockerImage" -}}
  {{- if .Values.global.useICPPrivateImages -}}
    {{/* assume image exists in ICP Private Registry */}}
    {{- printf "mycluster.icp:8500/default/bluecompute-orders" -}}
    {{/*{{- printf "mycluster.icp:8500/%s/bluecompute-orders" .Release.Namespace - */}}
  {{- else -}}
    {{- .Values.image.repository }}
  {{- end }}
{{- end -}}

{{- define "dataLoaderDockerImage" -}}
  {{- if .Values.global.useICPPrivateImages -}}
    {{/* assume image exists in ICP Private Registry */}}
    {{- printf "mycluster.icp:8500/default/bluecompute-dataloader" -}}
    {{/*- printf "mycluster.icp:8500/%s/bluecompute-dataloader" .Release.Namespace - */}}
  {{- else -}}
    {{- .Values.dataloader.image.repository }}
  {{- end }}
{{- end -}}