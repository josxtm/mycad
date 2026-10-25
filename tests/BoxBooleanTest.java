import es.josxtm.mycad.BoxBoolean;
public class BoxBooleanTest {
 static void near(double actual,double expected){if(Math.abs(actual-expected)>1e-8)throw new AssertionError(actual+" != "+expected);}
 public static void main(String[] args){
  BoxBoolean.Box a=new BoxBoolean.Box(0,0,0,10,10,10);
  BoxBoolean.Box b=new BoxBoolean.Box(5,0,0,10,10,10);
  near(BoxBoolean.overlap(a,b),500);
  near(BoxBoolean.unionVolume(a,b),1500);
  near(BoxBoolean.differenceVolume(a,b),500);
  BoxBoolean.Box c=new BoxBoolean.Box(30,0,0,10,10,10);
  near(BoxBoolean.overlap(a,c),0);
  near(BoxBoolean.unionVolume(a,c),2000);
  BoxBoolean.Box d=new BoxBoolean.Box(0,0,0,5,5,5);
  near(BoxBoolean.overlap(a,d),125);
  near(BoxBoolean.differenceVolume(a,d),875);
  System.out.println("Box Boolean volume tests OK");
 }
}
