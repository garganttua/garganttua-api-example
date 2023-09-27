package com.garganttua.api.example;

import java.util.Date;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import com.garganttua.api.security.keys.GGAPIKeyType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeyDTO {
	
	@Field
	@Indexed(unique=true)
	private String uuid;
	
	@Field
	private String realm;
	
	@Field
	private String algorithm;
	
	@Field
	private Date expiration;
	
	@Field
	private GGAPIKeyType type;
	
	@Field
	private byte[] encoded;

}
