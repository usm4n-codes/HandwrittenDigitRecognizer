"""Train and convert an MNIST CNN; Android inference code is Java-only."""
from pathlib import Path
import tensorflow as tf
root=Path(__file__).resolve().parents[1];out=root/"app/src/main/assets/digit_model.tflite"
(x,y),(xt,yt)=tf.keras.datasets.mnist.load_data()
x=(x.astype("float32")/255.)[...,None];xt=(xt.astype("float32")/255.)[...,None]
m=tf.keras.Sequential([tf.keras.layers.Input((28,28,1)),tf.keras.layers.Conv2D(16,3,activation="relu"),tf.keras.layers.MaxPooling2D(),tf.keras.layers.Conv2D(32,3,activation="relu"),tf.keras.layers.MaxPooling2D(),tf.keras.layers.Flatten(),tf.keras.layers.Dense(64,activation="relu"),tf.keras.layers.Dense(10,activation="softmax")])
m.compile(optimizer="adam",loss="sparse_categorical_crossentropy",metrics=["accuracy"])
m.fit(x,y,epochs=3,batch_size=128,validation_split=.1)
_,acc=m.evaluate(xt,yt,verbose=0);print(f"MNIST test accuracy: {acc*100:.2f}%")
data=tf.lite.TFLiteConverter.from_keras_model(m).convert();out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(data);print(f"Wrote {len(data)} bytes to {out}")
