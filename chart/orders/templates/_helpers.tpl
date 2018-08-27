{{- define "orders.fullname" -}}
  {{- .Release.Name }}-{{ .Chart.Name -}}
{{- end -}}

{{/* MySQL Init Container Template */}}
{{- define "orders.labels" }}
app: bluecompute
micro: orders
tier: backend
heritage: {{ .Release.Service | quote }}
release: {{ .Release.Name | quote }}
chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{- end }}

{{/* MySQL Init Container Template */}}
{{- define "orders.mysql.initcontainer" }}
- name: test-mysql
  image: {{ .Values.ordersmysql.image }}:{{ .Values.ordersmysql.imageTag }}
  imagePullPolicy: {{ .Values.ordersmysql.imagePullPolicy }}
  command:
  - "/bin/bash"
  - "-c"
  {{- if .Values.ordersmysql.mysqlPassword }}
  - "until mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e status; do echo waiting for mysql; sleep 1; done"
  {{- else }}
  - "until mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u${MYSQL_USER} -e status; do echo waiting for mysql; sleep 1; done"
  {{- end }}
  env:
  {{- include "orders.mysql.environmentvariables" . | indent 2 }}
{{- end }}

{{/* Orders MySQL Environment Variables */}}
{{- define "orders.mysql.environmentvariables" }}
- name: MYSQL_HOST
  value: {{ .Values.ordersmysql.fullnameOverride | quote }}
- name: MYSQL_PORT
  value: {{ .Values.ordersmysql.service.port | quote }}
- name: MYSQL_DATABASE
  value: {{ .Values.ordersmysql.mysqlDatabase | quote }}
- name: MYSQL_USER
  value: {{ .Values.ordersmysql.mysqlUser | quote }}
{{- if .Values.ordersmysql.mysqlPassword }}
- name: MYSQL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Values.ordersmysql.fullnameOverride | quote }}
      key: mysql-password
{{- end }}
{{- end }}

{{/* Orders HS256KEY Environment Variables */}}
{{- define "orders.hs256key.environmentvariables" }}
- name: HS256_KEY
  valueFrom:
    secretKeyRef:
        name: {{ template "orders.hs256key.secretName" . }}
        key:  key
{{- end }}

{{/* Orders HS256KEY Secret Name */}}
{{- define "orders.hs256key.secretName" -}}
  {{- if .Values.global.hs256key.secretName -}}
    {{ .Values.global.hs256key.secretName -}}
  {{- else if .Values.hs256key.secretName -}}
    {{ .Values.hs256key.secretName -}}
  {{- else -}}
    {{- .Release.Name }}-{{ .Chart.Name }}-hs256key
  {{- end }}
{{- end -}}