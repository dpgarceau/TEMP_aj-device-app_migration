FROM eclipse-temurin:17-jdk-alpine
RUN adduser judge;echo 'judge:P@ssword1234$' | chpasswd
RUN mkdir -p /var/opt/judge; chown judge:judge /var/opt/judge
VOLUME /tmp
EXPOSE 8080
USER judge
COPY --chown=judge:judge figures/ /var/opt/judge/figures/
# Maven package copies the versioned build output to dockerbuild/app.jar.
# The runtime name stays judge.jar to match device service behavior.
COPY --chown=judge:judge dockerbuild/app.jar /var/opt/judge/bin/judge.jar
COPY --chown=judge:judge dockerbuild/settings.json /var/opt/judge/settings.json
COPY --chown=judge:judge scripts/judge_update.sh /home/judge/judge_update.sh
COPY --chown=judge:judge scripts/fetch_update.sh /home/judge/fetch_update.sh
RUN mkdir -p /var/opt/judge/pilots/scores
ENTRYPOINT ["java","-jar","/var/opt/judge/bin/judge.jar"]
