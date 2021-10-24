package com.github.slamdev.bomversionsextractor;

import java.util.Objects;

public class ArtifactInfo {
    private final String group;
    private final String name;
    private final String version;

    public ArtifactInfo(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactInfo that = (ArtifactInfo) o;
        return group.equals(that.group) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name);
    }
}
