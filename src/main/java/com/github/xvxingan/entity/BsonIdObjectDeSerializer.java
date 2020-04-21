package com.github.xvxingan.entity;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import org.bson.types.ObjectId;

import java.lang.reflect.Type;

/**
 * BsonId 的反序列化方法
 * @author xuxingan
 */
public class BsonIdObjectDeSerializer implements ObjectDeserializer {


    @Override
    public ObjectId deserialze(DefaultJSONParser defaultJSONParser, Type type, Object o) {
        Object input = defaultJSONParser.parse("id");
        ObjectId bsonId = new ObjectId(input.toString());
        return bsonId;
    }

    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
