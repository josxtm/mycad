import es.josxtm.mycad.cad.CadKernel;
import es.josxtm.mycad.cad.PrimitiveKernel;

public final class CadKernelTest {
 private static void check(boolean pass,String message){if(!pass)throw new AssertionError(message);}
 public static void main(String[] args){
  CadKernel kernel=new PrimitiveKernel();
  CadKernel.Result<CadKernel.Box> first=kernel.createBox(0,0,0,20,10,5);
  check(first.isSuccess(),"No se crea caja");
  check(Math.abs(first.value.volume()-1000)<1e-9,"Volumen incorrecto");
  check(Math.abs(first.value.surfaceArea()-700)<1e-9,"Área incorrecta");
  CadKernel.Result<CadKernel.Box> resized=kernel.resizeHeight(first.value,8);
  check(resized.isSuccess()&&resized.value.volume()==1600,"Cambio de altura");
  check(first.value.id.equals(resized.value.id),"ID no conservado");
  check(first.value.volume()==1000,"El cuerpo original fue modificado");
  check(kernel.createBox(0,0,0,0,10,5).code==CadKernel.Code.INVALID_GEOMETRY,"Ancho cero");
  check(kernel.createBox(0,0,0,-1,10,5).code==CadKernel.Code.INVALID_GEOMETRY,"Ancho negativo");
  check(kernel.createBox(Double.NaN,0,0,1,1,1).code==CadKernel.Code.INVALID_GEOMETRY,"NaN");
  check(kernel.resizeHeight(first.value,Double.POSITIVE_INFINITY).code==CadKernel.Code.INVALID_GEOMETRY,"Infinito");
  System.out.println("CadKernelTest: OK");
 }
}
