package org.lakehouse.config.entities;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

@MappedSuperclass
public abstract class KeyEntityAbstract {
	@Id
	private String keyName;

	private String description;

	public KeyEntityAbstract() {
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return getKeyName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof KeyEntityAbstract keyEntity))
			return false;
		return Objects.equals(getKeyName(), keyEntity.getKeyName())
				&& Objects.equals(getDescription(), keyEntity.getDescription());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getKeyName(), getDescription());
	}

	public boolean equalsByName(KeyEntityAbstract that) {
		return Objects.equals(this.getKeyName(), that.getKeyName());
	}
}
