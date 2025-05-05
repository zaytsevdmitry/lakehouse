package org.lakehouse.config.entities;

import jakarta.persistence.Entity;

import java.util.Objects;

@Entity
public class DataStore extends KeyEntityAbstract {

	private String interfaceType;

	private String vendor;

	private String url;
	public DataStore() {
	}

	public String getInterfaceType() {
		return interfaceType;
	}

	public void setInterfaceType(String interfaceType) {
		this.interfaceType = interfaceType;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public boolean equals(Object o) {

		DataStore DataStore = (DataStore) o;
		return super.equals(o) && Objects.equals(getInterfaceType(), DataStore.getInterfaceType())
				&& Objects.equals(getVendor(), DataStore.getVendor())
				&& Objects.equals(getUrl(), DataStore.getUrl());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getInterfaceType(), getVendor(),getUrl());
	}
}
