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

volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0;   //-------p2

void partirMinuterie(uint16_t duree)
{
    //minuterieExpiree = 0;

    TCNT1 = 0; //0x1FF; // valeur de départ

    OCR1A = duree;

    TCCR1A = 0; //(1 << COM1A1) | (1 << COM1A0);

    TCCR1B = (1 << WGM12) | (1 << CS12) | (1 << CS10);

    TCCR1C = 0;

    TIMSK1 = 1 << OCIE1A;
}
/*void initialisation(){
	cli();
	boutonPoussoir = 0;
    // PORT A est en mode sortie
   DDRA = 0xff;
   DDRB = 0xff; // PORT B est en mode sortie
   DDRC = 0xff; // PORT C est en mode sortie
   DDRD = 0x00; // PORT D est en mode entree
   
  // DDRD = 1<<INT0;
   //PORTD = 1<<INT0;
	
//   PORTA = 0 << INT0;
	
   EIMSK |= (1 << INT0) ;
   EICRA |= (1 << ISC00); //le 00 suit le mouvement du front montant et descendant
   // le 01 ne fait que suivre le front descendant ---> ne respecte pas le socnditions du tp ne pas prendre poour le probleme 1
   //le 10 fait l'entierete des etapes en continu sans besoin d'appuyere ----> ne prend pas les conditions if()
   //11 same thing
	//partirMinuterie(78906);

	}
*/

ISR(TIMER1_COMPA_vect)
{
    minuterieExpiree = 1;
    boutonPoussoir = 0;
    
    cli(); 
}

ISR(INT0_vect)
{
    _delay_ms(30);
    minuterieExpiree = 0;
    
    boutonPoussoir = 1;
    cli(); 
}

int main()
{
    cli();

    boutonPoussoir = 0;
    // PORT A est en mode sortie
    DDRA = 0xff;
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree

    EIMSK |= (1 << INT0);
    EICRA |= (1 << ISC00); //le 00 suit le mouvement du front montant et descendant
                           // le 01 ne fait que suivre le front descendant ---> ne respecte pas le socnditions du tp ne pas prendre poour le probleme 1
                           //le 10 fait l'entierete des etapes en continu sans besoin d'appuyere ----> ne prend pas les conditions if()
                           //11 same thing

    PORTA = 0b00;
    _delay_ms(10000);
    PORTA = 0b10;
    _delay_ms(100);
    PORTA = 0b00;
    
    partirMinuterie(7810);
    EIFR |= (1 << INTF0);//ou 0	 
    sei();
    while (minuterieExpiree == 0 && boutonPoussoir == 0)
    {
    
    }
       cli(); 
        if (boutonPoussoir == 1)
        {
            PORTA = 0b01;    
        }
        else if (minuterieExpiree == 1)
        {
            PORTA = 0b10;
        }
    return 0;
}
