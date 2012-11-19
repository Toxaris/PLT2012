/* 
 * The Power and Responsibility of Direct-Environment-Passing Style
 *
 * Author: Yi Dai
 * Date: 2012-11-18
 */

sealed abstract class Imp
case class Nml(num : Int) extends Imp
case class Smb(nom : Symbol) extends Imp
case class Pls(lhs : Imp, rhs : Imp) extends Imp
case class If0(cnd : Imp, csq : Imp, alt : Imp) extends Imp
case class Lam(prm : Symbol, bod : Imp) extends Imp
case class Cmb(opr : Imp, opd : Imp) extends Imp
case class Seq(zth : Imp, fst : Imp) extends Imp
case class Set(nom : Symbol, dfn : Imp) extends Imp

type Env = List[(Symbol, Imp)]

case class Clo(env : Env, prm : Symbol, bod : Imp) extends Imp

def Let(nom : Symbol, dfn : Imp, bod : Imp) : Imp = Cmb(Lam(nom, bod), dfn)

def isNormal(imp : Imp) : Boolean = imp match {
  case Nml(_) => true
  case Clo(_, _, _) => true
  case _ => false
}

def lookup[A, B](a : A, al : List[(A, B)]) : B = {
  if (al.isEmpty)
    sys.error("no association found for: " + a)
  else if (((al.head)_1) == a)
    (al.head)_2
  else
    lookup(a, al.tail)
}
 
def update[A, B](a : A, b : B, al : List[(A, B)]) : List[(A, B)] = {
  if (al.isEmpty)
    List()
  else if (((al.head)_1) == a)
    (a, b) :: al.tail
  else
    al.head :: update(a, b, al.tail)
}

def norm(imp : Imp, env : Env) : (Imp, Env) = imp match {
  case Nml(_) => (imp, env)
  case Clo(_, _, _) => (imp, env)
  case Smb(nom) => (lookup(nom, env), env)
  case Pls(lhs, rhs)=> {
    val (nf0, env0) = norm(lhs, env)
    nf0 match {
      case Nml(num0) => {
        val (nf1, env1) = norm(rhs, env0)
        nf1 match {
          case Nml(num1) => (Nml(num0 + num1), env1)
          case _ => sys.error("not a number designator: " + rhs)
        }
      }
      case _ => sys.error("not a number designator: " + lhs)
    }
  }
  case If0(cnd, csq, alt) => {
    val (nf0, env0) = norm(cnd, env)
    nf0 match {
      case Nml(0) => norm(csq, env0)
      case Nml(_) => norm(alt, env0) 
      case _ => sys.error("not a number designator: " + cnd)
    }
  }
  case Lam(prm, bod) => (Clo(env, prm, bod), env)
  case Cmb(opr, opd) => {
    val (nf0, env0) = norm(opr, env)
    nf0 match {
      case Clo(cenv, prm, bod) => {
        val (nf1, env1) = norm(opd, env0)
        val (nf, lenv) = norm(bod, (prm, nf1) :: cenv)
        (nf, opr match {
               case Smb(nom) => update(nom, Clo(lenv.tail, prm, bod), env1)
               case _ => env1
             } )
      }
      case _ => sys.error("not a function designator: " + opr)
    }
  }
  case Seq(zth, fst) => {
    val (_, env0) = norm(zth, env)
    norm(fst, env0)
  }
  case Set(nom, dfn) => {
    val (nf, env0) = norm(dfn, env)
    (nf, update(nom, nf, env0))
  }
}

def emEnv : Env = List()

/*
 * (let ((x 1))
 *   (+ (let ((x 2)) x)
 *      x ) )
 */

def test0 : Imp =
  Let( 'x, Nml(1)
     , Pls( Let('x, Nml(2), Smb('x)) 
          , Smb('x) ) )

/*
 * (let ((x 1))
 *   (let ((f (lambda (y) (+ x y))))
 *     (set! x 2)
 *     (f 3) ) )
 */

def test1 : Imp =
  Let( 'x, Nml(1)
     , Let( 'f, Lam('y, Pls(Smb('x), Smb('y)))
          , Seq( Set('x, Nml(2))
               , Cmb(Smb('f), Nml(3)) ) ) )

/*
 * (let ((counter (lambda (y)
 *                  (let ((z y))
 *                    (lambda (s)
 *                      (if (= s 0)
 *                          z
 *                          (begin (set! z (+ z s))
 *                                 z ) ) ) ) ) ) )
 *    (let ((count (counter 0)))
 *      (count 1)
 *      (count 2) ) )
 */

def test2 : Imp =
  Let( 'counter, Lam('y, Let( 'z, Smb('y)
                            , Lam('s, If0( Smb('s)
                                         , Smb('z)
                                         , Seq( Set('z, Pls(Smb('z), Smb('s)))
                                              , Smb('z) ) ) ) ) )
     , Let( 'count, Cmb(Smb('counter), Nml(0))
          , Seq( Cmb(Smb('count), Nml(1))
               , Cmb(Smb('count), Nml(2)) ) ) )

/*
 * (let ((x 1))
 *   (let ((f (lambda (y)
 *              (set! x (+ x y)) ) ) )
 *     (f 2)
 *     x ) )
 */

def test3 : Imp =
  Let( 'x, Nml(1)
     , Let( 'f, Lam('y, Set('x, Pls(Smb('x), Smb('y))))
          , Seq( Cmb(Smb('f), Nml(2))
               , Smb('x) ) ) )

