package jeremy_replica.friendly_end;

/**
* friendly_end/FlightReservationServerHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from C:/Users/Jeremy/Source/Repos/SOEN423_Project/Jeremy_Replica/src/FlightReservationServer.idl
* Sunday, December 4, 2016 6:10:02 PM EST
*/

public final class FlightReservationServerHolder implements org.omg.CORBA.portable.Streamable
{
  public jeremy_replica.friendly_end.FlightReservationServer value = null;

  public FlightReservationServerHolder ()
  {
  }

  public FlightReservationServerHolder (jeremy_replica.friendly_end.FlightReservationServer initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = jeremy_replica.friendly_end.FlightReservationServerHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    jeremy_replica.friendly_end.FlightReservationServerHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return jeremy_replica.friendly_end.FlightReservationServerHelper.type ();
  }

}
