package lakehouse.api.entities.configs;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

@MappedSuperclass
public abstract class KeyEntityAbstract {
	@Id
	private String name;

	private String description;

	public KeyEntityAbstract() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof KeyEntityAbstract keyEntity))
			return false;
		return Objects.equals(getName(), keyEntity.getName())
				&& Objects.equals(getDescription(), keyEntity.getDescription());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getDescription());
	}

	public boolean equalsByName(KeyEntityAbstract that) {
		return Objects.equals(this.getName(), that.getName());
	}
}
