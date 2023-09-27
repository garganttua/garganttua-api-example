package com.garganttua.api.example;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "access_tokens")
public class TokenDTO{
	
	@Id
	@Indexed(unique=true)
	private String uuid;
	
	@Field
	private String userUuid;
	
	@Field
	private Date creationDate;
	
	@Field
	private Date expirationDate;
	
	@Field
	private byte[] token;
	
	@Field
	private String signingKeyId;
}
