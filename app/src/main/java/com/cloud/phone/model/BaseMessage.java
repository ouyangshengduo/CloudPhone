package com.cloud.phone.model;
import com.google.gson.Gson;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author senduo.ouyang
 * @date 2020-10-12
 * @version 1.0
 */
public abstract class BaseMessage<T,E>{

    private MessageType messageType;//消息类型
    private T message;              //消息主体
    private E extra;                //附加字段

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }

    public E getExtra() {
        return extra;
    }

    public void setExtra(E extra) {
        this.extra = extra;
    }

    //对象转成json
    public String toJson(){
        Gson gson = new Gson();
        Type[] types = getClassType(getClass());
        Map<String,String> map = new HashMap<>();
        map.put("messageType",gson.toJson(messageType,MessageType.class));
        map.put("message",gson.toJson(message,types[0]));
        map.put("extra",gson.toJson(extra,types[1]));
        return gson.toJson(map);
    }

    //获取泛型实际参数类型
    protected Type[] getClassType(Class<?> clazz){
        //获取直接超类的类型，包含泛型参数，如：class A extends HttpResponse<List<String>>(){}
        //那么getGenericSuperclass() = HttpResponse<List<String>>
        Type type = clazz.getGenericSuperclass();
        //如果是参数化类型则
        if(type instanceof ParameterizedType){
            //强转
            ParameterizedType parameterizedType = (ParameterizedType) type;
            //获取类型中的实际参数类型的数组,因为泛型有可能有多个
            Type[] typeArguments = parameterizedType.getActualTypeArguments();

            return typeArguments;
        }
        throw new IllegalArgumentException(BaseMessage.class.getName() + "的直接子类必须指定泛型参数！");
    }
}
