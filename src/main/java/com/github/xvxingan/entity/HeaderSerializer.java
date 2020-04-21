package com.github.xvxingan.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.apache.http.Header;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BsonIdObjectSerializer 的序列化方法
 * @author xuxingan
 */
public class HeaderSerializer implements ObjectSerializer {


    @Override
    public void write(JSONSerializer jsonSerializer, Object object, Object fieldName, Type fieldType, int i) throws IOException {
        SerializeWriter writer = jsonSerializer.getWriter();
        if(object instanceof List && ((List)object).get(0) instanceof Header){
            List<Header> objectList = (List<Header>) object;
            List<Map<String, String>> collect = objectList.stream().map(item -> {
                return Collections.singletonMap(item.getName(), item.getValue());
            }).collect(Collectors.toList());
            writer.write(JSON.toJSONString(collect));
        }else {
            writer.writeNull();
        }
    }
}
