FROM ubuntu:20.04
ENV DEBIAN_FRONTEND=noninteractive

# 1. Sistem Güncellemesi ve Temel Araçlar
# - OpenMVG derlemek için gerekli C++ araçları
# - Videoları parçalamak için FFmpeg
# - Build scriptleri için temel Python3 (Kütüphanesiz)
RUN apt-get update && apt-get install -y \
    build-essential cmake git \
    libpng-dev libjpeg-dev libtiff-dev \
    libglu1-mesa-dev libxxf86vm-dev libxi-dev libxrandr-dev \
    ffmpeg \
    python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

# 2. OpenMVG Kaynak Kodunu İndir
RUN git clone --recursive https://github.com/openMVG/openmvg.git /opt/openmvg

# 3. OpenMVG Derleme (Build) İşlemi
WORKDIR /opt/openmvg_build
RUN cmake -DCMAKE_BUILD_TYPE=RELEASE /opt/openmvg/src \
    && make -j$(nproc)

# 4. Komutları Sisteme Tanıt (PATH ayarı)
ENV PATH="/opt/openmvg_build/Linux-x86_64-RELEASE:${PATH}"

# Çalışma dizini
WORKDIR /data