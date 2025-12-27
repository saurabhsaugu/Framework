FROM maven:3.8.8-openjdk-17

WORKDIR /workspace

# Install Chrome and required system libraries so Selenium/ChromeDriver can run inside the container
USER root
RUN apt-get update \
    && apt-get install -y wget gnupg ca-certificates unzip fonts-liberation libxss1 libasound2 libatk1.0-0 libatk-bridge2.0-0 libgtk-3-0 libgbm-dev lsb-release \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Copy project sources
COPY . /workspace

# Optionally pre-download dependencies to speed up test runs
RUN mvn -q -DskipTests dependency:go-offline

# Default command runs tests in headless mode using the web driver (override at runtime)
CMD ["mvn", "test", "-Ddriver=web", "-Dheadless=true"]
