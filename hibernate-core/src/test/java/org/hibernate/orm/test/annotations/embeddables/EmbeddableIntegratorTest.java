/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.embeddables;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Pheby
 */
@BaseUnitTest
@RequiresDialect(H2Dialect.class)
public class EmbeddableIntegratorTest {

	/**
	 * Throws a mapping exception because DollarValue is not mapped
	 */
	@Test
	public void testWithoutIntegrator() {
		try (SessionFactory sf = new Configuration().addAnnotatedClass( Investor.class )
				.setProperty( "hibernate.hbm2ddl.auto", "create-drop" )
				.buildSessionFactory()) {
			Session sess = sf.openSession();
			try {
				sess.getTransaction().begin();
				Investor myInv = getInvestor();
				myInv.setId( 1L );

				sess.save( myInv );
				sess.flush();
				fail( "A JDBCException expected" );

				sess.clear();

				Investor inv = sess.get( Investor.class, 1L );
				assertEquals( new BigDecimal( "100" ), inv.getInvestments().get( 0 ).getAmount().getAmount() );
			}
			catch (PersistenceException e) {
				assertTyping( JDBCException.class, e );
				sess.getTransaction().rollback();
			}
			finally {
				if ( sess.getTransaction().isActive() ) {
					sess.getTransaction().rollback();
				}
			}
			sess.close();
		}
	}

	@Test
	public void testWithTypeContributor() {
		try (SessionFactory sf = new Configuration().addAnnotatedClass( Investor.class )
				.registerTypeContributor( new InvestorTypeContributor() )
				.setProperty( "hibernate.hbm2ddl.auto", "create-drop" )
				.buildSessionFactory(); Session sess = sf.openSession()) {
			try {
				sess.getTransaction().begin();
				Investor myInv = getInvestor();
				myInv.setId( 2L );

				sess.save( myInv );
				sess.flush();
				sess.clear();

				Investor inv = sess.get( Investor.class, 2L );
				assertEquals( new BigDecimal( "100" ), inv.getInvestments().get( 0 ).getAmount().getAmount() );
			}
			finally {
				if ( sess.getTransaction().isActive() ) {
					sess.getTransaction().rollback();
				}
			}
		}
	}

	private Investor getInvestor() {
		Investor i = new Investor();
		List<Investment> investments = new ArrayList<>();
		Investment i1 = new Investment();
		i1.setAmount( new DollarValue( new BigDecimal( "100" ) ) );
		i1.setDate( new MyDate( new Date() ) );
		i1.setDescription( "Test Investment" );
		investments.add( i1 );
		i.setInvestments( investments );

		return i;
	}
}
