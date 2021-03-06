package dogs.bedazzle

trait BedazzleBirds {
  implicit def toBirdOps[A](a: A) = new BirdOps(a)
}

class BirdOps[A](val a: A) extends AnyVal {

  def |>[B](f: A => B): B = f(a)

  def unsafeTap(f: A => Unit): A = {
    f(a)
    a
  }

  def <|(f: A => Unit): A = {
    f(a)
    a
  }
}
