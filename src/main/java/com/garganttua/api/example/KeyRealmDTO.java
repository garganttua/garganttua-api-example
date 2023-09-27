package com.garganttua.api.example;

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
@Document(collection = "key_realms")
public class KeyRealmDTO {
	
	@Id
	@Indexed(unique=true)
	private String name;
	
	@Field
	private String algo;
	
	@Field
	private KeyDTO cipheringKey;
	
	@Field
	private KeyDTO uncipheringKey;

}
