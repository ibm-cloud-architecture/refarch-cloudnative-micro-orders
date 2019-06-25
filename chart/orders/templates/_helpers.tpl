{{/* MySQL Init Container Template */}}
{{- define "orders.mariadb.initcontainer" }}
- name: test-mariadb
  image: {{ .Values.mysql.image }}:{{ .Values.mysql.imageTag }}
  imagePullPolicy: {{ .Values.mysql.imagePullPolicy }}
  command:
  - "/bin/bash"
  - "-c"
  {{- if .Values.mariadb.db.password }}
  - "until mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e status; do echo waiting for mariadb; sleep 1; done"
  {{- else }}
  - "until mysql -h ${MYSQL_HOST} -P ${MYSQL_PORT} -u${MYSQL_USER} -e status; do echo waiting for mariadb; sleep 1; done"
  {{- end }}
  env:
  {{- include "orders.mariadb.environmentvariables" . | indent 2 }}
{{- end }}

{{/* Orders MySQL Environment Variables */}}
{{- define "orders.mariadb.environmentvariables" }}
- name: MYSQL_HOST
  value: {{ .Release.Name }}-{{ .Values.service.ordersdb }}
- name: MYSQL_PORT
  value: {{ .Values.mariadb.service.port | quote }}
- name: MYSQL_DATABASE
  value: {{ .Values.mariadb.db.name | quote }}
- name: MYSQL_USER
  value: {{ .Values.mariadb.user | quote }}
{{- if or .Values.mariadb.db.password .Values.mariadb.existingSecret }}
- name: MYSQL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Name }}-{{ .Values.service.name }}-mariadb-secret
      key: mariadb-password
{{- end }}
{{- end }}
