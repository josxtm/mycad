package es.josxtm.mycad.cad;

import java.util.UUID;

/** First isolated implementation. Supports valid boxes only; NOT a B-Rep kernel. */
public final class PrimitiveKernel implements CadKernel {
 private static boolean finite(double n){return !Double.isNaN(n)&&!Double.isInfinite(n);}
 private static boolean valid(double x,double y,double z,double w,double d,double h){
  return finite(x)&&finite(y)&&finite(z)&&finite(w)&&finite(d)&&finite(h)
   &&Math.abs(x)<=100000&&Math.abs(y)<=100000&&Math.abs(z)<=100000
   &&w>0&&d>0&&h>0&&w<=100000&&d<=100000&&h<=100000;
 }
 @Override public Result<Box> createBox(double x,double y,double z,double w,double d,double h){
  if(!valid(x,y,z,w,d,h))return Result.invalid("Dimensiones o coordenadas no válidas");
  return Result.success(new Box(UUID.randomUUID().toString(),x,y,z,w,d,h));
 }
 @Override public Result<Box> resizeHeight(Box box,double height){
  if(box==null)return Result.invalid("Cuerpo ausente");
  if(!valid(box.x,box.y,box.z,box.width,box.depth,height))return Result.invalid("Altura no válida");
  return Result.success(new Box(box.id,box.x,box.y,box.z,box.width,box.depth,height));
 }
}
