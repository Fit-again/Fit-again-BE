#!/bin/bash

APP_DIR="/home/ubuntu/app"
JAR_DIR="$APP_DIR/build"
JAR_FILE=$(ls -tr $JAR_DIR/*SNAPSHOT.jar | tail -n 1)
LOG_FILE="$APP_DIR/application.log"

echo "======================================" > $LOG_FILE
echo "배포 시작 시간: $(date)" >> $LOG_FILE
echo "배포할 JAR 파일: $JAR_FILE" >> $LOG_FILE

CURRENT_PID=$(pgrep -f "java -jar")
if [ -z "$CURRENT_PID" ]; then
    echo "> 현재 실행 중인 애플리케이션이 없습니다." >> $LOG_FILE
else
    echo "> 실행 중인 애플리케이션(PID: $CURRENT_PID) 종료 중..." >> $LOG_FILE
    kill -15 $CURRENT_PID
    sleep 5
fi

cd $APP_DIR

if [ -f /home/ubuntu/app/.env ]; then
    echo "> .env 파일을 로드합니다." >> $LOG_FILE
    while IFS='=' read -r key value || [ -n "$key" ]; do
        if [[ ! -z "$key" && "$key" != \#* ]]; then
            # 양끝 공백 및 따옴표 완벽 제거
            value=$(echo "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
            key=$(echo "$key" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
            export "$key=$value"
        fi
    done < /home/ubuntu/app/.env
else
    echo "> 경고: .env 파일을 찾을 수 없습니다!" >> $LOG_FILE
fi

# 파싱된 변수 디버깅 (비밀번호 제외하고 로그에 출력하여 원인 파악)
echo "[디버깅] 파싱된 DB_URL 값: ->${DB_URL}<-" >> $LOG_FILE

nohup java -jar $JAR_FILE >> $LOG_FILE 2>&1 &

echo "> 배포 완료. Spring Boot 부팅 대기 중..." >> $LOG_FILE
