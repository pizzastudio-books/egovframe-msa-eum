{{/*
이름과 라벨을 한 곳에서 만든다.

매니페스트를 손으로 쓰던 때는 라벨을 스물몇 곳에 똑같이 적었다. 한 곳을 빠뜨리면
서비스가 파드를 못 찾는데, 오타는 배포가 끝난 뒤에야 드러난다. 여기서 한 번 정의하고
전부 여기를 부른다.
*/}}

{{- define "eum.fullname" -}}
{{- printf "%s-%s" .Release.Name .svcName | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "eum.labels" -}}
app.kubernetes.io/name: {{ include "eum.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: eum
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "eum.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eum.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
이미지 이름. 태그를 적지 않으면 차트의 appVersion 을 쓴다.

태그에 latest 를 쓰지 않는 이유는 20.1 에 있다. 어느 판이 돌고 있는지 알 수 없으면
장애가 났을 때 되돌릴 곳도 알 수 없다.
*/}}
{{- define "eum.image" -}}
{{- $reg := .root.Values.image.registry -}}
{{- $tag := .svc.image.tag | default .root.Chart.AppVersion -}}
{{- if $reg -}}
{{ $reg }}/{{ .svc.image.repository }}:{{ $tag }}
{{- else -}}
{{ .svc.image.repository }}:{{ $tag }}
{{- end -}}
{{- end -}}

{{/*
데이터베이스 계정. 서비스마다 다르다(13.2).
*/}}
{{- define "eum.dbEnv" -}}
{{- if .svc.database }}
- name: MYSQL_USER
  valueFrom:
    secretKeyRef:
      name: {{ .root.Release.Name }}-secret
      key: {{ .svcName | upper | replace "-" "_" }}_MYSQL_USER
- name: MYSQL_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .root.Release.Name }}-secret
      key: {{ .svcName | upper | replace "-" "_" }}_MYSQL_PASSWORD
{{- end }}
{{- end -}}

{{/*
브로커 계정. 메시지를 주고받는 서비스에만 넣는다.
*/}}
{{- define "eum.brokerEnv" -}}
{{- if .svc.messaging }}
- name: RABBITMQ_USER
  valueFrom:
    secretKeyRef:
      name: {{ .root.Release.Name }}-secret
      key: RABBITMQ_USER
- name: RABBITMQ_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .root.Release.Name }}-secret
      key: RABBITMQ_PASSWORD
{{- end }}
{{- end -}}
