package com.papaya.claims.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.papaya.claims.contract.*;
import com.papaya.claims.contract.Model.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Repository public final class MyBatisCommandRepository implements CommandRepository { private final CommandMapper mapper; private final ObjectMapper json; public MyBatisCommandRepository(CommandMapper mapper,ObjectMapper json){this.mapper=mapper;this.json=json;} public Optional<CommandReceipt> find(UUID id,String user,String key){var r=mapper.find(id,user,key);return r==null?Optional.empty():Optional.of(PersistenceJson.receipt(json,r.result()));} public void insert(UUID id,String user,String key,CommandReceipt receipt){mapper.insert(id,user,key,receipt.requestHash(),PersistenceJson.write(json,receipt));} }
