package es.josxtm.mycad.cad;

/** Android-independent solid-kernel contract, dimensions are in millimetres. */
public interface CadKernel {
 enum Code { SUCCESS, INVALID_GEOMETRY, UNSUPPORTED, FAILED }
 final class Result<T> {
  public final Code code;
  public final T value;
  public final String message;
  private Result(Code code,T value,String message){this.code=code;this.value=value;this.message=message;}
  public static <T> Result<T> success(T value){return new Result<>(Code.SUCCESS,value,"");}
  public static <T> Result<T> invalid(String message){return new Result<>(Code.INVALID_GEOMETRY,null,message);}
  public static <T> Result<T> unsupported(String message){return new Result<>(Code.UNSUPPORTED,null,message);}
  public boolean isSuccess(){return code==Code.SUCCESS;}
 }
 final class Box {
  public final String id;
  public final double x,y,z,width,depth,height;
  public Box(String id,double x,double y,double z,double width,double depth,double height){
   this.id=id;this.x=x;this.y=y;this.z=z;this.width=width;this.depth=depth;this.height=height;
  }
  public double volume(){return width*depth*height;}
  public double surfaceArea(){return 2*(width*depth+width*height+depth*height);}
 }
 Result<Box> createBox(double x,double y,double z,double width,double depth,double height);
 Result<Box> resizeHeight(Box box,double height);
}
