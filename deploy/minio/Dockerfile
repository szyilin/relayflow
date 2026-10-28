# syntax=docker/dockerfile:1
# The upstream container registries no longer serve these community releases.
# Build unchanged official source at immutable commits, verified by Go's sumdb.
ARG GO_IMAGE=golang@sha256:8bee1901f1e530bfb4a7850aa7a479d17ae3a18beb6e09064ed54cfd245b7191
ARG ALPINE_IMAGE=alpine@sha256:5b10f432ef3da1b8d4c7eb6c487f2f5a8f096bc91145e68878dd4a5019afde11
FROM ${GO_IMAGE} AS tooling
ENV CGO_ENABLED=0 GOTOOLCHAIN=local GOPROXY=https://proxy.golang.org GOSUMDB=sum.golang.org
WORKDIR /src

FROM tooling AS minio-build
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download github.com/minio/minio@v0.0.0-20250422221226-0d7408fc9969 \
    && cp -R /go/pkg/mod/github.com/minio/minio@v0.0.0-20250422221226-0d7408fc9969/. /src/
RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build \
    go build -mod=readonly -trimpath -buildvcs=false -o /out/minio \
    -ldflags '-s -w -X github.com/minio/minio/cmd.Version=2025-04-22T22:12:26Z -X github.com/minio/minio/cmd.ReleaseTag=RELEASE.2025-04-22T22-12-26Z -X github.com/minio/minio/cmd.CommitID=0d7408fc9969caf07de6a8c3a84f9fbb10a6739e -X github.com/minio/minio/cmd.ShortCommitID=0d7408fc9969 -X github.com/minio/minio/cmd.CopyrightYear=2025' .

FROM tooling AS mc-build
RUN --mount=type=cache,target=/go/pkg/mod \
    go mod download github.com/minio/mc@v0.0.0-20250813083541-7394ce0dd2a8 \
    && cp -R /go/pkg/mod/github.com/minio/mc@v0.0.0-20250813083541-7394ce0dd2a8/. /src/
RUN --mount=type=cache,target=/go/pkg/mod --mount=type=cache,target=/root/.cache/go-build \
    go build -mod=readonly -trimpath -buildvcs=false -o /out/mc \
    -ldflags '-s -w -X github.com/minio/mc/cmd.Version=2025-08-13T08:35:41Z -X github.com/minio/mc/cmd.ReleaseTag=RELEASE.2025-08-13T08-35-41Z -X github.com/minio/mc/cmd.CommitID=7394ce0dd2a80935aded936b09fa12cbb3cb8096 -X github.com/minio/mc/cmd.ShortCommitID=7394ce0dd2a8 -X github.com/minio/mc/cmd.CopyrightYear=2025' .

FROM ${ALPINE_IMAGE} AS minio
LABEL org.opencontainers.image.source=https://github.com/minio/minio \
      org.opencontainers.image.revision=0d7408fc9969caf07de6a8c3a84f9fbb10a6739e \
      org.opencontainers.image.licenses=AGPL-3.0-only
COPY --from=minio-build /out/minio /usr/bin/minio
COPY --from=minio-build /src/LICENSE /licenses/minio-LICENSE
VOLUME /data
EXPOSE 9000 9001
ENTRYPOINT ["minio"]

FROM ${ALPINE_IMAGE} AS mc
LABEL org.opencontainers.image.source=https://github.com/minio/mc \
      org.opencontainers.image.revision=7394ce0dd2a80935aded936b09fa12cbb3cb8096 \
      org.opencontainers.image.licenses=AGPL-3.0-only
COPY --from=mc-build /out/mc /usr/bin/mc
COPY --from=mc-build /src/LICENSE /licenses/mc-LICENSE
ENTRYPOINT ["mc"]
