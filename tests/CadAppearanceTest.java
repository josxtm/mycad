import es.josxtm.mycad.ui.CadAppearance;
import java.util.*;
public class CadAppearanceTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static void reject(Runnable op){try{op.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError();}
 public static void main(String[] args){
  check(CadAppearance.NAMES.length==6);Set<Integer> backgrounds=new HashSet<>();
  for(int i=0;i<5;i++){
   CadAppearance p=CadAppearance.preset(i);backgrounds.add(p.color(CadAppearance.Role.BACKGROUND));
   for(CadAppearance.Role r:CadAppearance.Role.values()){int color=p.color(r);check(color>>>24==255);check(CadAppearance.contrast(p.ink(color),color)>=4.5);check(CadAppearance.parse(CadAppearance.hex(color))==color);}
   int[] copy=p.colors();copy[0]=0;check(p.colors()[0]!=0);
  }
  check(backgrounds.size()==5);check(!CadAppearance.preset(0).dark());check(CadAppearance.preset(1).dark());
  CadAppearance custom=CadAppearance.preset(4).with(CadAppearance.Role.MODEL,0xff123456);check(custom.color(CadAppearance.Role.MODEL)==0xff123456);
  check(CadAppearance.normalize(-1)==0&&CadAppearance.normalize(6)==0);check(CadAppearance.normalize(5)==5);
  reject(()->CadAppearance.parse("#123"));reject(()->CadAppearance.parse("#ff123456"));reject(()->CadAppearance.parse("red"));
  reject(()->CadAppearance.custom(new int[12]));reject(()->CadAppearance.custom(new int[0]));
  System.out.println("CadAppearanceTest: OK");
 }
}
