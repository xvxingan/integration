package com.github.xvxingan.entity;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * BsonId 的序列化方法
 * @author xuxingan
 */
public class BsonIdObjectSerializer implements ObjectSerializer {


    @Override
    public void write(JSONSerializer jsonSerializer, Object object, Object fieldName, Type fieldType, int i) throws IOException {
        SerializeWriter writer = jsonSerializer.getWriter();
        if(object instanceof ObjectId){
            ObjectId objectId = (ObjectId) object;
            writer.write("\""+objectId.toString()+"\"");
        }else {
            writer.writeNull();
        }
    }
}
