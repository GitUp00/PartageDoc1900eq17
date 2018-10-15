// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)

#include <avr/interrupt.h>
#include <avr/io.h>
#define F_CPU 8000000
#include <util/delay.h>
#include "memoire_24.h"



void initialisationUART ( void ) {

	// 2400 bauds. Nous vous donnons la valeur des deux

	// premier registres pour vous éviter des complications

	UBRR0H = 0;

	UBRR0L = 0xCF;

	// permettre la reception et la transmission par le UART0

	UCSR0A = 0 ;

	UCSR0B = (1<<RXEN0)|(1<<TXEN0) ;
	// Format des trames: 8 bits, 1 stop bits, none parity

	UCSR0C = (1<<USBS0)|(3<<UCSZ00) ;

	}
	
	void transmissionUART ( uint8_t data ) {

	/* Wait for empty transmit buffer */
	while ( !( UCSR0A & (1<<UDRE0)) )
	;
	/* Put data into buffer, sends the data */
	UDR0 = data;

	}	


int main()
{
	
	
	
    DDRA = 0xff;
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    
    initialisationUART();
    Memoire24CXXX entete;
    /*
    char mots[21] = "Le robot en INF1900\n";
	*/
	uint8_t i, j;
	
	char polyLecture[21] ;
    /*
    lecture(const uint16_t adresse, uint8_t *donnee,
                   const uint8_t longueur);
                   
    ecriture(const uint16_t adresse, uint8_t *donnee,
                    const uint8_t longueur);
    */
    
    
    bool vrai = true;
    
    char polyEcriture[] = "Le robot en INF1900\n";//"*P*O*L*Y*T*E*C*H*N*I*Q*U*E* *M*O*N*T*R*E*A*L*" ;
    entete.ecriture(0x00, (uint8_t*)polyEcriture,sizeof(polyEcriture));
    
    _delay_ms(5);
    
    entete.lecture(0x00,(uint8_t*)polyLecture,sizeof(polyLecture));
    for (int i = 0; i < 21; i++)
	{
		if (polyEcriture[i] == polyLecture[i])
		{
			vrai = true;
		}
	}
	
	if (vrai)
	{
		PORTA = 0b01;
	}
	else
		PORTA = 0b10;
	for ( i = 0; i < 100; i++ ) {

		for ( j=0; j < 20; j++ ) {

		transmissionUART(polyEcriture[j]);

		}

	}
}


