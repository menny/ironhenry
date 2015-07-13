package versionbuilder

class VersionBuilder {
    static final int GIT_COMMIT_COUNT_NORMALIZE = 0;
    static final int GIT_COMMIT_COUNT_MINOR_NORMALIZE = 0;

    static def buildGitVersionNumber() {
        return Integer.parseInt('git rev-list --count HEAD'.execute().text.trim()) - GIT_COMMIT_COUNT_NORMALIZE;
    }

    static def buildGitVersionName() {
        return String.format("%d.%d.%d", 0, 1, buildGitVersionNumber() - GIT_COMMIT_COUNT_MINOR_NORMALIZE);
    }

}