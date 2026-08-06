FROM python:3.13-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# 安装 Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制代码
COPY . .

# 创建数据目录
RUN mkdir -p data/uploads data/novels data/covers data/apk

EXPOSE 8000

# 启动: 先初始化数据库，再启动服务
CMD ["sh", "-c", "python -c 'from database import init_db; from init_data import seed_data; init_db(); seed_data()' && uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}"]
