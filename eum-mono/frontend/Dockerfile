# 이음 화면 이미지.
#
# 두 단계로 만든다. 앞 단계에서 빌드하고, 뒤 단계에는 결과 파일만 담는다. node 와
# node_modules 가 최종 이미지에 남지 않아 크기가 크게 줄고, 반입 심사에서 볼 것도
# 줄어든다(20.3).
#
#   docker build -t eum/eum-web:1.0.0 .

FROM node:20-alpine AS build

WORKDIR /app

# package.json 이 바뀌지 않으면 npm ci 단계를 다시 돌리지 않는다.
# 소스만 고쳤을 때 빌드가 빨라진다.
COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 8080
