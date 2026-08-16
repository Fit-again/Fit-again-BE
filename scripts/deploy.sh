#!/bin/bash

# 1. 환경 변수 및 경로 설정
APP_DIR="/home/ubuntu/app"
JAR_DIR="$APP_DIR/build"
JAR_FILE=$(ls -tr $JAR_DIR/*SNAPSHOT.jar | tail -n 1)
LOG_FILE="$APP_DIR/application.log"

echo "======================================"
echo "배포 시작 시간: $(date)"
echo "배포할 JAR 파일: $JAR_FILE"

# 2. 실행 중인 Spring Boot 애플리케이션 종료
CURRENT_PID=$(pgrep -f "java -jar")
if [ -z "$CURRENT_PID" ]; then
    echo "> 현재 실행 중인 애플리케이션이 없습니다."
else
    echo "> 실행 중인 애플리케이션(PID: $CURRENT_PID) 종료 중..."
    kill -15 $CURRENT_PID
    sleep 5
fi

# 3. 새 애플리케이션 백그라운드 실행
echo "> 새 애플리케이션 실행 준비..."
cd $APP_DIR

# EC2 내부에 숨겨둔 .env 파일을 안전하게 파싱하여 환경변수로 적용 (특수기호 에러 방지)
if [ -f /home/ubuntu/app/.env ]; then
    echo "> .env 파일을 안전하게 로드합니다."
    while IFS='=' read -r key value; do
        if [[ ! -z "$key" && "$key" != \#* ]]; then
            # 양끝 따옴표 제거 (따옴표가 있어도 무시)
            value="${value%\"}"
            value="${value#\"}"
            export "$key=$value"
        fi
    done < /home/ubuntu/app/.env
else
    echo "> 경고: .env 파일을 찾을 수 없습니다!"
fi

# 백그라운드(nohup)로 실행
nohup java -jar $JAR_FILE > $LOG_FILE 2>&1 &

echo "> 배포가 성공적으로 완료되었습니다!"
echo "======================================"
